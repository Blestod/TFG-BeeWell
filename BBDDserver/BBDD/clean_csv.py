import pandas as pd
import re
import unicodedata

def limpiar_nombre(nombre):
    if not isinstance(nombre, str):
        return ""
    # Eliminar todo lo que no sea letras, números, espacios, guiones o comas
    nombre = re.sub(r'[^a-zA-Z0-9\s\-\,]', '', nombre)
    # Normalizar tildes y letras acentuadas
    nombre = unicodedata.normalize('NFKD', nombre).encode('ascii', 'ignore').decode('ascii')
    return nombre.strip()

def limpiar_csv(input_path, output_path):
    try:
        df = pd.read_csv(input_path, sep=",", encoding="utf-8", on_bad_lines="skip", engine="python")
    except Exception as e:
        print("❌ Error al leer el CSV:", e)
        return

    if "ingredient_name" not in df.columns:
        print("⚠️ No se encontró la columna 'ingredient_name' en el archivo.")
        return

    print("🧹 Limpiando nombres de ingredientes...")
    df["ingredient_name"] = df["ingredient_name"].apply(limpiar_nombre)

    df.to_csv(output_path, sep=",", index=False)
    print(f"✅ CSV limpio guardado en: {output_path}")

if __name__ == "__main__":
    entrada = input("📄 Ruta del CSV a limpiar (ej: ingredientes_openfood.csv): ").strip()
    salida = input("📁 Nombre del archivo de salida limpio (ej: ingredientes_openfood_clean.csv): ").strip()
    limpiar_csv(entrada, salida)