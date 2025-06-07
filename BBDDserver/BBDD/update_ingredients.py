import csv
from tqdm import tqdm
from tables import Food, db
from main import api

MAX_LEN = 255

def actualizar_ingredientes_desde_csv(ruta_csv: str = "ingredients_cleaned.csv") -> None:
    """
    Lee un CSV con información nutricional y sincroniza la tabla Food:
      · actualiza los campos vacíos de alimentos ya existentes
      · crea nuevos registros si aún no existen
    """
    with open(ruta_csv, newline="", encoding="utf-8") as csvfile:
        lector = csv.DictReader(csvfile, delimiter=",")

        # ───────────────────────────────────────────────────────────
        # 1 ·  cargar todo lo que ya hay en la BD
        # ───────────────────────────────────────────────────────────
        print("📥 Cargando alimentos existentes de la base de datos...")
        alimentos_existentes = {f.name.lower(): f for f in Food.query.all()}

        count_nuevos       = 0
        count_actualizados = 0
        procesados         = 0

        # ───────────────────────────────────────────────────────────
        # 2 ·  recorrer filas del CSV
        # ───────────────────────────────────────────────────────────
        for row in tqdm(lector, desc="⏳ Procesando filas"):
            # el CSV original usa `ingredient_name`; admitimos también `name`
            nombre = row.get("ingredient_name") or row.get("name")
            if not nombre:
                continue

            nombre = nombre[:MAX_LEN].strip()
            if not nombre:
                continue

            clave   = nombre.lower()
            alimento = alimentos_existentes.get(clave)

            # ── a) el alimento ya existe → rellenar campos que falten
            if alimento:
                actualizado = False

                if not alimento.estim_carbs and row.get("estim_carbs"):
                    alimento.estim_carbs = float(row["estim_carbs"])
                    actualizado = True
                if not alimento.estim_protein and row.get("estim_protein"):
                    alimento.estim_protein = float(row["estim_protein"])
                    actualizado = True
                if not alimento.estim_fats and row.get("estim_fats"):
                    alimento.estim_fats = float(row["estim_fats"])
                    actualizado = True
                if not alimento.i_g and row.get("i_g"):
                    alimento.i_g = float(row["i_g"])
                    actualizado = True

                if actualizado:
                    count_actualizados += 1

            # ── b) alimento nuevo → insert
            else:
                nuevo = Food(
                    name=nombre,
                    estim_carbs=float(row["estim_carbs"])   if row.get("estim_carbs")   else None,
                    estim_protein=float(row["estim_protein"]) if row.get("estim_protein") else None,
                    estim_fats=float(row["estim_fats"])     if row.get("estim_fats")     else None,
                    i_g=float(row["i_g"])                   if row.get("i_g")           else None
                )
                db.session.add(nuevo)
                alimentos_existentes[clave] = nuevo
                count_nuevos += 1

            procesados += 1
            # commit intermedio para no llenar el buffer
            if procesados % 10_000 == 0:
                db.session.commit()
                print(f"💾 Commit intermedio después de {procesados} filas...")

        # ───────────────────────────────────────────────────────────
        # 3 ·  commit final y resumen
        # ───────────────────────────────────────────────────────────
        db.session.commit()
        print(f"\n✔ Alimentos nuevos añadidos: {count_nuevos}")
        print(f"✔ Alimentos actualizados:     {count_actualizados}")


# ───────────────────────────────────────────────────────────────────
#  módulo ejecutable
# ───────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    with api.app_context():
        actualizar_ingredientes_desde_csv()
        print("\n✅ ¡Actualización finalizada!")
