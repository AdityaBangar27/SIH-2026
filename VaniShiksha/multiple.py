import matplotlib.pyplot as plt
import pandas as pd
from sklearn.linear_model import LinearRegression

data = {
    'Experience': [1, 2, 3, 4, 5],
    'Test_Score': [80, 85, 88, 92, 95],
    'Salary': [30000, 37000, 42000, 48000, 53000]
}
df = pd.DataFrame(data)

x = df[['Experience', 'Test_Score']]
y = df['Salary']

model = LinearRegression()
model.fit(x, y)

new_data = pd.DataFrame({'Experience': [3], 'Test_Score': [90]})
predicted_salary = model.predict(new_data)[0]
print(f"New prediction (3 yrs exp, 90 score): ${round(predicted_salary, 2):,}")

candidate_6yr = pd.DataFrame({'Experience': [6], 'Test_Score': [95]})
y_pred_6yr = model.predict(candidate_6yr)[0]
print(f"New prediction (6 yrs exp, 95 score): ${round(y_pred_6yr, 2):,}")