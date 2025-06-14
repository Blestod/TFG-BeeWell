#!/usr/bin/env python3
import csv
import requests
from tqdm import tqdm

# ╭─────────────────────────── CONFIG ───────────────────────────╮
API_BASE      = "https://beewell.blestod.com"
CSV_PATH      = "/Users/thomas/Documents/GitHub/TFG-BeeWell/BBDDserver/BBDD/saved_data/vitals_dump_user.csv"
FORCE_EMAIL   = "user"
BATCH_SIZE    = 500
# ╰──────────────────────────────────────────────────────────────╯

def clean_float(value: str):
    try:
        return float(value) if value.strip() != "" else None
    except Exception:
        return None

def build_payload(row: dict) -> dict:
    return {
        "user_email":       FORCE_EMAIL,
        "vital_time":       int(row["vital_time"]),
        "glucose_value":    clean_float(row.get("glucose_value", "")),
        "heart_rate":       clean_float(row.get("heart_rate", "")),
        "temperature":      clean_float(row.get("temperature", "")),
        "calories":         clean_float(row.get("calories", "")),
        "sleep_duration":   clean_float(row.get("sleep_duration", "")),
        "oxygen_saturation": clean_float(row.get("oxygen_saturation", ""))
    }

def post_vital(payload: dict):
    url = f"{API_BASE}/vital"
    r = requests.post(url, json=payload, timeout=10)
    if not r.ok:
        raise RuntimeError(f"POST /vital → {r.status_code} {r.text}")

def importar_csv():
    total, ok, err, skipped = 0, 0, 0, 0
    sent_times = set()

    with open(CSV_PATH, newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)

        buffer = []
        for row in tqdm(reader, desc="⏩ Enviando"):
            total += 1
            try:
                payload = build_payload(row)
                vital_time = payload["vital_time"]

                if vital_time in sent_times:
                    skipped += 1
                    continue

                buffer.append(payload)
                sent_times.add(vital_time)

                if len(buffer) >= BATCH_SIZE:
                    for p in buffer:
                        post_vital(p)
                    ok += len(buffer)
                    buffer.clear()

            except Exception as e:
                err += 1
                print(f"⚠️  Error fila {total}: {e}")

        # enviar lo que queda
        for p in buffer:
            try:
                post_vital(p)
                ok += 1
            except Exception as e:
                err += 1
                print(f"⚠️  Error final: {e}")

    print(f"\n🏁 Terminado.")
    print(f"   ➤ Filas en CSV: {total}")
    print(f"   ➤ Enviadas correctamente: {ok}")
    print(f"   ➤ Duplicadas omitidas: {skipped}")
    print(f"   ➤ Con errores: {err}")

if __name__ == "__main__":
    importar_csv()
