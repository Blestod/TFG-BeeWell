import pandas as pd

def fusionar_csv(nuevo_csv, final_csv="ingredientes_final.csv"):
    try:
        print("📂 Leyendo archivo base:", final_csv)
        df_final = pd.read_csv(final_csv)
    except FileNotFoundError:
        print("⚠️ No existe ingredientes_final.csv, se creará uno nuevo.")
        df_final = pd.DataFrame(columns=["ingredient_name", "estim_carbs", "estim_protein", "estim_fats", "i_g"])

    print("📥 Leyendo nuevo archivo:", nuevo_csv)
    df_nuevo = pd.read_csv(nuevo_csv, sep=";", on_bad_lines="skip", engine="python")

    # Crear claves en minúscula para emparejar por nombre
    df_final["key"] = df_final["ingredient_name"].str.lower()
    df_nuevo["key"] = df_nuevo["ingredient_name"].str.lower()

    dict_final = df_final.set_index("key").to_dict("index")
    registros_actualizados = []

    for _, row in df_nuevo.iterrows():
        key = row["key"]
        if key in dict_final:
            original = dict_final[key]
            actualizado = {
                "ingredient_name": original["ingredient_name"],
                "estim_carbs": original["estim_carbs"] if pd.notna(original["estim_carbs"]) else row["estim_carbs"],
                "estim_protein": original["estim_protein"] if pd.notna(original["estim_protein"]) else row["estim_protein"],
                "estim_fats": original["estim_fats"] if pd.notna(original["estim_fats"]) else row["estim_fats"],
                "i_g": original["i_g"] if pd.notna(original["i_g"]) else row["i_g"]
            }
        else:
            actualizado = {
                "ingredient_name": row["ingredient_name"],
                "estim_carbs": row["estim_carbs"],
                "estim_protein": row["estim_protein"],
                "estim_fats": row["estim_fats"],
                "i_g": row["i_g"]
            }
        registros_actualizados.append(actualizado)

    # Guardar en CSV actualizado
    df_resultado = pd.DataFrame(registros_actualizados)
    df_resultado.to_csv(final_csv, index=False)
    print(f"\n✅ ¡Fusionado exitosamente! Ingredientes totales: {len(df_resultado)}")
    print(f"📁 Archivo actualizado: {final_csv}")

if __name__ == "__main__":
    nuevo = input("📄 Nombre del archivo nuevo a fusionar (ej: ingredientes_openfood.csv): ").strip()
    fusionar_csv(nuevo)
