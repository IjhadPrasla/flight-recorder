import argparse
import json
import math
import sys
import time
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timezone
from typing import Iterator

import grpc
from google.protobuf.timestamp_pb2 import Timestamp

import telemetry_pb2
import telemetry_pb2_grpc


def create_session(api_url: str) -> str:
    session_name = (
        "Simulator Run "
        + datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    )

    request_body = json.dumps({"name": session_name}).encode("utf-8")

    request = urllib.request.Request(
        f"{api_url}/api/sessions",
        data=request_body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    with urllib.request.urlopen(request, timeout=10) as response:
        session = json.loads(response.read().decode("utf-8"))

    return session["id"]


def generate_readings(
    session_id: str,
    vehicle_count: int,
    readings_per_vehicle: int,
    rate_hz: float,
) -> Iterator[telemetry_pb2.TelemetryReading]:
    interval_seconds = 1.0 / rate_hz

    base_latitude = 30.2672
    base_longitude = -97.7431

    for sequence_number in range(readings_per_vehicle):
        cycle_started = time.perf_counter()

        for vehicle_index in range(vehicle_count):
            vehicle_id = f"vehicle-{vehicle_index + 1:03d}"
            phase = (
                sequence_number * 0.08
                + vehicle_index * (2 * math.pi / vehicle_count)
            )

            recorded_at = Timestamp()
            recorded_at.FromDatetime(datetime.now(timezone.utc))

            latitude = base_latitude + 0.01 * math.sin(phase)
            longitude = base_longitude + 0.01 * math.cos(phase)
            speed_kph = max(0.0, 45.0 + 15.0 * math.sin(phase * 1.5))
            battery_percent = max(
                0.0,
                100.0 - sequence_number * 0.03 - vehicle_index * 2.0,
            )
            motor_temperature = 55.0 + 8.0 * math.sin(phase * 0.7)

            yield telemetry_pb2.TelemetryReading(
                reading_id=str(uuid.uuid4()),
                session_id=session_id,
                vehicle_id=vehicle_id,
                recorded_at=recorded_at,
                latitude=latitude,
                longitude=longitude,
                speed_kph=speed_kph,
                battery_percent=battery_percent,
                motor_temperature_celsius=motor_temperature,
                sequence_number=sequence_number,
            )

        elapsed = time.perf_counter() - cycle_started
        time.sleep(max(0.0, interval_seconds - elapsed))


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Stream simulated vehicle telemetry to Flight Recorder."
    )

    parser.add_argument("--grpc-host", default="localhost:9090")
    parser.add_argument("--api-url", default="http://localhost:8080")
    parser.add_argument("--vehicles", type=int, default=3)
    parser.add_argument("--readings", type=int, default=100)
    parser.add_argument("--rate", type=float, default=10.0)

    arguments = parser.parse_args()

    if arguments.vehicles <= 0:
        parser.error("--vehicles must be greater than zero")

    if arguments.readings <= 0:
        parser.error("--readings must be greater than zero")

    if arguments.rate <= 0:
        parser.error("--rate must be greater than zero")

    return arguments


def main() -> int:
    arguments = parse_arguments()

    try:
        session_id = create_session(arguments.api_url)
    except urllib.error.URLError as exception:
        print(
            f"Could not create a session through {arguments.api_url}: "
            f"{exception}",
            file=sys.stderr,
        )
        return 1

    total_readings = arguments.vehicles * arguments.readings

    print(f"Created session: {session_id}")
    print(
        f"Streaming {total_readings} readings from "
        f"{arguments.vehicles} vehicles at "
        f"{arguments.rate:.1f} Hz per vehicle..."
    )

    started = time.perf_counter()

    try:
        with grpc.insecure_channel(arguments.grpc_host) as channel:
            grpc.channel_ready_future(channel).result(timeout=10)

            client = (
                telemetry_pb2_grpc.TelemetryIngestionServiceStub(channel)
            )

            timeout_seconds = max(
                30.0,
                arguments.readings / arguments.rate + 15.0,
            )

            summary = client.StreamReadings(
                generate_readings(
                    session_id=session_id,
                    vehicle_count=arguments.vehicles,
                    readings_per_vehicle=arguments.readings,
                    rate_hz=arguments.rate,
                ),
                timeout=timeout_seconds,
            )
    except grpc.RpcError as exception:
        print(
            f"gRPC stream failed: {exception.code().name}: "
            f"{exception.details()}",
            file=sys.stderr,
        )
        return 1
    except TimeoutError:
        print(
            f"Could not connect to gRPC server at "
            f"{arguments.grpc_host}",
            file=sys.stderr,
        )
        return 1

    elapsed = time.perf_counter() - started
    throughput = summary.accepted_count / elapsed if elapsed > 0 else 0.0

    print(
        f"Complete: {summary.accepted_count} accepted, "
        f"{summary.rejected_count} rejected"
    )
    print(f"Elapsed: {elapsed:.2f} seconds")
    print(f"Observed throughput: {throughput:.1f} readings/second")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
