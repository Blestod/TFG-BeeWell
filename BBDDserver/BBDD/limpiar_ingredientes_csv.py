import pandas as pd

MAX_LEN = 255
INPUT_CSV = "ingredients.csv"
OUTPUT_CSV = "ingredients_cleaned.csv"

def limpiar_ingredientes_csv(input_path=INPUT_CSV, output_path=OUTPUT_CSV):
    print(f"📥 Leyendo archivo: {input_path}...")

    try:
        df = pd.read_csv(input_path)
    except FileNotFoundError:
        print(f"❌ Archivo no encontrado: {input_path}")
        return
    except Exception as e:
        print(f"❌ Error al leer el CSV: {e}")
        return

    # Normalizar nombre
    df["ingredient_name"] = df["ingredient_name"].astype(str).str.lower().str.strip()
    df["ingredient_name"] = df["ingredient_name"].str.slice(0, MAX_LEN)

    # Eliminar duplicados por nombre
    df_cleaned = df.drop_duplicates(subset=["ingredient_name"], keep="first")

    # Guardar
    df_cleaned.to_csv(output_path, index=False)
    print(f"✅ Archivo limpio guardado como: {output_path}")
    print(f"🧾 Ingredientes originales: {len(df)}")
    print(f"🧼 Ingredientes únicos:     {len(df_cleaned)}")

if __name__ == "__main__":
    limpiar_ingredientes_csv()
