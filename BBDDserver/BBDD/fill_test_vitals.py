# fill_test_vitals.py

from tables import db, User, Vital
from main import api  # importa tu Flask app
from datetime import datetime, timedelta
import time

with api.app_context():
    # Verifica que el usuario 'test' exista
    user = User.query.get("test")
    if not user:
        print("⚠️ El usuario 'test' no existe en la base de datos.")
        exit()

    end_date = datetime(2025, 5, 24)
    start_date = end_date - timedelta(days=89)  # últimos 90 días

    current = start_date
    added = 0

    while current <= end_date:
        ts = int(time.mktime(current.timetuple()))  # timestamp (segundos desde epoch)

        vital = Vital(
            user_email="test",
            vital_time=ts,
            glucose_value=100
        )
        db.session.add(vital)
        added += 1
        current += timedelta(days=1)

    db.session.commit()
    print(f"✅ Insertados {added} vitals para 'test'")
