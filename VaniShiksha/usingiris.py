import pandas as pd
from sklearn.datasets import load_iris
from sklearn.linear_model import LinearRegression
from sklearn.model_selection import train_test_split

iris = load_iris(as_frame=True)
df = iris.frame

X = df[['sepal length (cm)', 'sepal width (cm)', 'petal width (cm)']]
y = df['petal length (cm)']

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

model = LinearRegression()
model.fit(X_train, y_train)

new_sample = pd.DataFrame({
    'sepal length (cm)': [5.1],
    'sepal width (cm)': [3.5],
    'petal width (cm)': [0.2]
})

predicted_length = model.predict(new_sample)
print(f"Predicted Petal Length: {predicted_length[0]:.2f} cm")
