import requests

# Email del usuario
user_email = "user"

# URL del backend
BASE_URL = "https://beewell.blestod.com"

# Endpoints
MEALS_URL = f"{BASE_URL}/meal_recommendation_data/{user_email}"
INSULIN_URL = f"{BASE_URL}/insulin_recommendation_data/{user_email}"
EXERCISE_URL = f"{BASE_URL}/exercise_recommendation_data/{user_email}"

try:
    events = []

    # 📦 1. Pedir comidas
    meal_resp = requests.get(MEALS_URL)
    meal_resp.raise_for_status()
    meals_data = meal_resp.json()
    if isinstance(meals_data, list):
        events.extend(meals_data)

    # 📦 2. Pedir inyección reciente (si hay)
    insulin_resp = requests.get(INSULIN_URL)
    insulin_resp.raise_for_status()
    insulin_data = insulin_resp.json()
    if isinstance(insulin_data, dict) and insulin_data.get("type") == "insulin_injection":
        events.append(insulin_data)

    # 📦 3. Pedir ejercicios recientes
    exercise_resp = requests.get(EXERCISE_URL)
    exercise_resp.raise_for_status()
    exercise_data = exercise_resp.json()
    if isinstance(exercise_data, list):
        events.extend(exercise_data)

    print("✅ Eventos completos para patient_data:")
    for e in events:
        print(e)

except requests.exceptions.HTTPError as http_err:
    print(f"❌ Error HTTP: {http_err}")
except Exception as err:
    print(f"❌ Error general: {err}")
