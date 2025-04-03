from flask_sqlalchemy import SQLAlchemy

db: SQLAlchemy = SQLAlchemy()

class User(db.Model):
    __tablename__="user"

    email = db.Column(db.String(45),primary_key = True)
    password = db.Column(db.String(60))
    birth_date = db.Column(db.Integer,nullable=True)
    sex = db.Column(db.Boolean,nullable=True)
    insuline_type = db.Column(db.String(45),nullable=True)
    #db.relationship("nombre de la tabla","backref = x->crea una variable en tabla hijo que se llame x y que referencie al padre para meal.user o user.meal","al eliminar usuario se borran hijos")
    meals = db.relationship("meal",backref="user",cascade="all,delete")
    glucose_readings = db.relationship("glucose_reading",backref="user",cascade="all,delete")
    vitals = db.relationship("vital",backref="user",cascade="all,delete")
    predictions = db.relationship("prediction",backref="user",cascade="all,delete")

class Meal(db.Model):
    __tablename__="meal"

    meal_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    meal_time = db.Column(db.Integer)
    description = db.Column(db.String(300))

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))

class Ingredient(db.Model):
    __tablename__="ingredient"

    ingrediend_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    ingredient_name = db.Column(db.String(20))
    quantity = db.Column(db.Float)
    estim_carbs= db.Column(db.Float)
    estim_protein= db.Column(db.Float)
    estim_fats= db.Column(db.Float)

    ingredients = db.relationship("ingredient",backref="meal",cascade="all,delete")

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))
    meal_meal_id=db.Column(db.Integer,db.ForeignKey("meal.meal_id"))

class GlucoseReading(db.Model):
    __tablename__="glucose_reading"

    reading_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    glucose_time = db.Column(db.Integer)
    glucose_value = db.Column(db.Float)

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))

class Vital(db.Model):
    __tablename__="vital"

    vital_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    vital_time = db.Column(db.Integer)
    heart_rate = db.Column(db.Float,nullable=True)
    temperature = db.Column(db.Float,nullable=True)
    calories = db.Column(db.Float,nullable=True)
    diastolic = db.Column(db.Float,nullable=True)
    systolic = db.Column(db.Float,nullable=True)
    is_sleeping = db.Column(db.Boolean,nullable=True)

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))

class Prediction(db.Model):
    __tablename__="prediction"

    prediction_id = db.Column(db.Integer,primary_key = True, autoincrement = True)
    predict_time = db.Column(db.Integer)
    confidence_lvl = db.Column(db.Integer)
    forecast_time = db.Column(db.Integer)
    forecast = db.Column(db.String(300))

    user_email=db.Column(db.String(45),db.ForeignKey("user.email"))
