#!/usr/bin/env python3
import csv
import requests
from tqdm import tqdm

API_BASE      = "https://beewell.blestod.com"
CSV_PATH      = "/Users/thomas/Documents/GitHub/TFG-BeeWell/BBDDserver/BBDD/saved_data/vitals_cleaned.csv"
BATCH_SIZE    = 500

def clean_float(value: str):
    try:
        return float(value) if value.strip() != "" else None
    except Exception:
        return None

def build_payload(row: dict) -> dict:
    return {
        "user_email":       row["user_email"],
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
    total, ok, err = 0, 0, 0
    with open(CSV_PATH, newline="", encoding="utf-8") as fh:
        reader = csv.DictReader(fh)
        buffer = []
        for row in tqdm(reader, desc="⏩ Enviando"):
            total += 1
            try:
                payload = build_payload(row)
                buffer.append(payload)
                if len(buffer) >= BATCH_SIZE:
                    for p in buffer:
                        post_vital(p)
                    ok += len(buffer)
                    buffer.clear()
            except Exception as e:
                err += 1
                print(f"⚠️  Error fila {total}: {e}")
        for p in buffer:
            try:
                post_vital(p)
                ok += 1
            except Exception as e:
                err += 1
                print(f"⚠️  Error fila {total - len(buffer) + 1}: {e}")
    print(f"\n🏁 Terminado. Filas totales: {total}  ✔️ OK: {ok}  ❌ Errores: {err}")

if __name__ == "__main__":
    importar_csv()
