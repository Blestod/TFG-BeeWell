#!/usr/bin/env python3
# ──────────────────────────────────────────────────────────
#  IMPORTAR CSV → POST /vital  (usuario hard-codeado)
# ──────────────────────────────────────────────────────────
import csv
import requests
from tqdm import tqdm                     # pip install tqdm

# ╭─────────────────────────── CONSTANTES ───────────────────────────╮
API_BASE      = "https://beewell.blestod.com"     # dominio del backend
CSV_PATH      = "/Users/thomas/Documents/GitHub/TFG-BeeWell/BBDDserver/BBDD/saved_data/vitals_dump_test.csv"            # ruta al CSV de entrada
FORCE_EMAIL   = "user"             # ← el e-mail destino
BATCH_SIZE    = 500                               # filas entre commits
# ╰──────────────────────────────────────────────────────────────────╯


def clean_float(value: str):
    """Convierte a float si viene algo, si no devuelve None."""
    try:
        return float(value) if value.strip() != "" else None
    except Exception:
        return None


def build_payload(row: dict) -> dict:
    """
    Convierte una fila del DictReader en el JSON esperado por el endpoint /vital.
    TODOS los registros se asignan al e-mail de FORCE_EMAIL.
    """
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
    """Lanza el POST /vital; levanta excepción si no devuelve 2xx."""
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

                # envío por lotes opcional
                if len(buffer) >= BATCH_SIZE:
                    for p in buffer:
                        post_vital(p)
                    ok += len(buffer)
                    buffer.clear()

            except Exception as e:
                err += 1
                print(f"⚠️  Error fila {total}: {e}")

        # enviar lo que quede en el buffer
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
