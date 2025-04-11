from pyspark.sql import SparkSession
from pyspark.sql.functions import col
import os
import shutil


def main():
    spark = SparkSession.builder \
        .appName("Procesar CSV de Ingredientes") \
        .getOrCreate()

    # Solicitar al usuario la ruta del archivo
    path = input("📂 Introduce la ruta del archivo CSV: ")
    print("\n📥 Cargando archivo, esto puede tardar unos segundos...")
    df = spark.read.csv(path, header=True, sep="\t", inferSchema=True)

    print("\n📑 Columnas detectadas en el archivo:")
    for i, c in enumerate(df.columns):
        print(f"{i+1:>2}. {c}")

    # Columnas deseadas por el usuario
    columnas_objetivo = {
        "ingredient_name": "Nombre del ingrediente",
        "estim_carbs": "Carbohidratos (g)",
        "estim_protein": "Proteínas (g)",
        "estim_fats": "Grasas (g)",
        "i_g": "Índice Glucémico"
    }

    mapeo = {}
    for clave, descripcion in columnas_objetivo.items():
        while True:
            entrada = input(f"\n🧠 ¿Qué columna representa ➜ {descripcion}? Escribe el nombre exacto: ")
            if entrada in df.columns:
                mapeo[clave] = entrada
                break
            else:
                print("⚠️ Esa columna no existe. Intenta de nuevo.")

    # Crear nuevo DataFrame con columnas seleccionadas y renombradas
    df_filtrado = df.select([col(mapeo[k]).alias(k) for k in columnas_objetivo.keys()])

    # 🔍 Filtrar filas: eliminar si no hay nombre o si solo tiene nombre
    df_filtrado = df_filtrado.filter(
        (col("ingredient_name").isNotNull()) &
        (
            col("estim_carbs").isNotNull() |
            col("estim_protein").isNotNull() |
            col("estim_fats").isNotNull() |
            col("i_g").isNotNull()
        )
    )

    # Mostrar muestra del resultado
    print("\n🔎 Vista previa del CSV limpio:")
    df_filtrado.show(10)

    # Guardar
    salida = input("\n📁 ¿Cómo quieres llamar al archivo de salida? (ej: ingredientes_spark.csv): ")
    df_filtrado.coalesce(1).write.csv(salida, header=True, sep=";", mode="overwrite")
    print(f"\n✅ Archivo exportado con éxito: {salida}")

    # Buscar el archivo part-*.csv dentro de la carpeta de salida
    export_folder = salida
    export_files = os.listdir(export_folder)
    for filename in export_files:
        if filename.startswith("part-") and filename.endswith(".csv"):
            original_file = os.path.join(export_folder, filename)
            renamed_file = f"{export_folder}.csv"
            shutil.move(original_file, renamed_file)
            shutil.rmtree(export_folder)  # borrar carpeta temporal
            print(f"📁 Archivo final guardado como: {renamed_file}")
            break


if __name__ == "__main__":
    main()
