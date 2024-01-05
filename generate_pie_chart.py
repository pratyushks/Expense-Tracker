import matplotlib.pyplot as plt
import sys

category_amounts = sys.argv[1:]
category_to_amount = {}

for item in category_amounts:
    category, amount = item.split(":")
    if category in category_to_amount:
        category_to_amount[category] += float(amount)
    else:
        category_to_amount[category] = float(amount)

categories = list(category_to_amount.keys())
amounts = [category_to_amount[category] for category in categories]

plt.figure(figsize=(12, 12))
plt.pie(amounts, labels=categories, autopct='%1.1f%%', startangle=140)
plt.axis('equal')
plt.title('Expense Distribution')

plt.savefig('pie_chart.png')