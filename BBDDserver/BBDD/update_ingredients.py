# ──────────────────────────────────────────────────────────────────────────
#  import_foods.py  (antes actualizar_ingredientes_desde_csv)
# ──────────────────────────────────────────────────────────────────────────
import csv
from tqdm import tqdm
from tables import Food, db
from main import api

MAX_LEN = 255

def actualizar_alimentos_desde_csv(ruta_csv: str = "ingredients_cleaned.csv") -> None:
    """
    Sincroniza el CSV con la tabla Food:
      • Rellena campos vacíos de alimentos ya existentes
      • Inserta nuevos registros si no existen
    """
    with open(ruta_csv, newline="", encoding="utf-8") as csvfile:
        lector = csv.DictReader(csvfile)

        print("📥 Cargando alimentos existentes de la base de datos…")
        alimentos_existentes = {
            f.food_name.lower(): f                     # ← atributo correcto
            for f in Food.query.all()
        }

        nuevos, actualizados, procesados = 0, 0, 0

        for row in tqdm(lector, desc="⏳ Procesando filas"):
            nombre = row.get("food_name") or row.get("ingredient_name")
            if not nombre:
                continue

            nombre = nombre.strip()[:MAX_LEN]
            if not nombre:
                continue

            clave     = nombre.lower()
            alimento  = alimentos_existentes.get(clave)

            if alimento:                 # ── ya existe ──────────────────
                mod = False
                if not alimento.estim_carbs   and row.get("estim_carbs"):
                    alimento.estim_carbs   = float(row["estim_carbs"]);   mod = True
                if not alimento.estim_protein and row.get("estim_protein"):
                    alimento.estim_protein = float(row["estim_protein"]); mod = True
                if not alimento.estim_fats    and row.get("estim_fats"):
                    alimento.estim_fats    = float(row["estim_fats"]);    mod = True
                if not alimento.i_g          and row.get("i_g"):
                    alimento.i_g          = float(row["i_g"]);            mod = True
                if mod:
                    actualizados += 1
            else:                        # ── nuevo registro ─────────────
                nuevo = Food(
                    food_name     = nombre,
                    estim_carbs   = float(row["estim_carbs"])   if row.get("estim_carbs")   else None,
                    estim_protein = float(row["estim_protein"]) if row.get("estim_protein") else None,
                    estim_fats    = float(row["estim_fats"])    if row.get("estim_fats")    else None,
                    i_g           = float(row["i_g"])           if row.get("i_g")           else None
                )
                db.session.add(nuevo)
                alimentos_existentes[clave] = nuevo
                nuevos += 1

            procesados += 1
            if procesados % 10_000 == 0:
                db.session.commit()
                print(f"💾 Commit intermedio después de {procesados} filas…")

        db.session.commit()
        print(f"\n✔ Alimentos nuevos añadidos: {nuevos}")
        print(f"✔ Alimentos actualizados:     {actualizados}")

if __name__ == "__main__":
    with api.app_context():
        actualizar_alimentos_desde_csv()
        print("\n✅ ¡Actualización finalizada!")
