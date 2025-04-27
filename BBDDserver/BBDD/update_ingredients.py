import csv
from tqdm import tqdm
from tables import Ingredient, db
from main import api

MAX_LEN = 255

def normalize_name(name):
    return name.lower().strip() if isinstance(name, str) else ""

def actualizar_ingredientes_desde_csv(ruta_csv="ingredients_cleaned.csv"):
    with open(ruta_csv, newline='', encoding="utf-8") as csvfile:
        lector = csv.DictReader(csvfile, delimiter=",")

        print("📥 Cargando ingredientes existentes de la base de datos...")
        ingredientes_existentes = {
            i.ingredient_name.lower(): i for i in Ingredient.query.all()
        }

        count_nuevos = 0
        count_actualizados = 0
        procesados = 0

        for row in tqdm(lector, desc="⏳ Procesando filas"):
            nombre = row["ingredient_name"]
            if not nombre:
                continue
            nombre = nombre[:MAX_LEN]  # Truncar a 255 caracteres
            if len(nombre.strip()) == 0:
                continue

            clave = nombre.lower()
            ingrediente = ingredientes_existentes.get(clave)

            if ingrediente:
                actualizado = False
                if not ingrediente.estim_carbs and row["estim_carbs"]:
                    ingrediente.estim_carbs = float(row["estim_carbs"])
                    actualizado = True
                if not ingrediente.estim_protein and row["estim_protein"]:
                    ingrediente.estim_protein = float(row["estim_protein"])
                    actualizado = True
                if not ingrediente.estim_fats and row["estim_fats"]:
                    ingrediente.estim_fats = float(row["estim_fats"])
                    actualizado = True
                if not ingrediente.i_g and row["i_g"]:
                    ingrediente.i_g = float(row["i_g"])
                    actualizado = True
                if actualizado:
                    count_actualizados += 1
            else:
                nuevo = Ingredient(
                    ingredient_name=nombre,
                    estim_carbs=float(row["estim_carbs"]) if row["estim_carbs"] else None,
                    estim_protein=float(row["estim_protein"]) if row["estim_protein"] else None,
                    estim_fats=float(row["estim_fats"]) if row["estim_fats"] else None,
                    i_g=float(row["i_g"]) if row["i_g"] else None
                )
                db.session.add(nuevo)
                ingredientes_existentes[clave] = nuevo
                count_nuevos += 1

            procesados += 1
            if procesados % 10000 == 0:
                db.session.commit()
                print(f"💾 Commit intermedio después de {procesados} filas...")

        db.session.commit()
        print(f"\n✔ Ingredientes nuevos añadidos: {count_nuevos}")
        print(f"✔ Ingredientes actualizados: {count_actualizados}")

if __name__ == "__main__":
    with api.app_context():
        actualizar_ingredientes_desde_csv()
        print("\n✅ ¡Actualización finalizada!")
