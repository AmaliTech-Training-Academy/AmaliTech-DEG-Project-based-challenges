#  Veridi Logistics — "Last Mile" Delivery Performance Audit

> **Olist Brazilian E-Commerce Dataset · Google Colab · Tableau Public**

---

## A. Executive Summary

Veridi Logistics has a systemic delivery accuracy problem affecting customer satisfaction across all 27 Brazilian states. Approximately **23% of delivered orders miss their estimated arrival date**, and the situation is far from uniform: customers in remote northern states (AM, AP, RR) experience late deliveries at **5× the rate** of customers near the São Paulo distribution hub. The data clearly proves the CEO's intuition — the problem is both a broken ETA estimation algorithm and a structural carrier coverage failure in remote regions. Late deliveries have a direct, measurable impact on customer sentiment: orders delivered on time average **4.18★**, while super-late orders collapse to **1.54★**, with 61% of super-late orders receiving a 1-star review (Pearson r = +0.47, p < 0.001). Fixing the ETA algorithm alone — by adding state-specific buffer days based on historical actuals — would resolve the majority of negative customer perception without requiring any changes to physical carrier operations.

---

## B. Project Links

| Deliverable | Link |
|---|---|
|  **Notebook (Google Colab)** | https://colab.research.google.com/drive/1eyohcz2Es1zg1-mPfAUhcdIFJxsS_hVv?usp=sharing |
|  **Dashboard (Tableau Public)** | https://public.tableau.com/app/profile/ingabire.chadia/viz/VeridiLogisticsAudit/VeridiLogisticsOperationalAuditDashboard |
|  **Presentation (PDF/Slides)** | https://docs.google.com/presentation/d/1EiQF4E4oN6sMVNaOvciEgpKzKUA7QJ7FfxzdHw0G94M/edit?usp=sharing |
|  **Video Walkthrough (Optional)** |  |

## C. Technical Explanation

### Data Cleaning

The raw Olist dataset required the following cleaning steps before analysis:

**1. Date Parsing**
All five date columns in `olist_orders_dataset.csv` were parsed from strings to `datetime` using `pd.to_datetime(..., errors='coerce')`. The `errors='coerce'` argument converts unparseable values to `NaT` rather than raising exceptions, allowing the rest of the pipeline to proceed cleanly.

**2. Review Deduplication (1-to-many Join Fix)**
The `olist_order_reviews_dataset.csv` contains multiple reviews per `order_id` in some cases. A naïve left-join would inflate the orders table row count. To prevent this, reviews were sorted by `review_creation_date` and deduplicated using `.drop_duplicates(subset='order_id', keep='last')` — retaining only the most recent review per order. A `len()` assertion after joining confirms the master dataset exactly matches the original order count (99,441 rows).

**3. Filtering Non-Delivered Orders**
Orders with `order_status != 'delivered'` (canceled, unavailable, processing, invoiced, shipped) were excluded from the delay analysis, as there is no `order_delivered_customer_date` to measure against. These represent ~3% of all orders and are flagged separately rather than imputed.

**4. Missing Date Handling**
A small number of delivered orders had `NaT` in `order_delivered_customer_date` or `order_estimated_delivery_date` due to data entry gaps. These rows were dropped using `df.dropna(subset=[...])` rather than imputed, since imputing delivery dates would corrupt the core `Days_Difference` metric.

**5. Category Translation**
The `product_category_name` field in `olist_products_dataset.csv` is in Portuguese. It was merged with `product_category_name_translation.csv` on the `product_category_name` key. Any unmatched categories (Portuguese names not in the translation table) were retained as-is and formatted using `.str.replace('_', ' ').str.title()` to produce readable English-style labels.

---

### Candidate's Choice: Revenue at Risk Analysis

**Feature added:** A "Revenue at Risk" metric combining late delivery rate, order volume, and average order value into a single R$ figure per state and per product category.

**Why this matters to the business:**

A raw late-rate percentage is an incomplete signal for resource allocation. A state with a 48% late rate but only 80 orders (e.g., RR — Roraima) represents a smaller operational problem than São Paulo with an 8% late rate and 42,000 orders. Operations teams cannot prioritise repair efforts from percentages alone — they need to know where money is actually being lost.

The Revenue at Risk metric is calculated as:

```
Revenue_At_Risk = Is_Late × total_order_value
```

Summed by state and category, this produces a single R$ number representing the transaction value at risk from poor customer experience. The analysis revealed that:

- **São Paulo** carries R$ 4.8M in revenue at risk — the highest in absolute terms — despite having the lowest late rate, purely because of order volume.
- **Remote northern states** (AM, AP, RR) have the worst late rates but lower absolute revenue at risk due to thin order volume.
- **Office Furniture** and **Electronics** are the highest-risk categories by the combined metric.

This insight directly informs the prioritisation of carrier SLA negotiations: fix São Paulo's last-mile efficiency to recover the largest absolute revenue, while fixing northern state ETA estimates to recover customer satisfaction scores.

---

## D. Story Completion Summary

| Story | Requirement | Status |
|---|---|---|
| **Story 1** | Join Orders + Reviews + Customers without row duplication | ✅ Complete |
| **Story 2** | Days_Difference column, On Time / Late / Super Late classification, handle missing values | ✅ Complete |
| **Story 3** | % late per state, map/bar chart, remote vs core insight | ✅ Complete |
| **Story 4** | Delay vs review score visualisation, avg score by status | ✅ Complete |
| **Bonus** | English category translation, category late-rate chart | ✅ Complete |
| **Candidate's Choice** | Revenue at Risk analysis — justified above | ✅ Complete |

---

## E. Pre-Submission Checklist

- [ ] GitHub repository is **Public** *(verify in Incognito window)*
- [ ] `.ipynb` notebook file uploaded to repo
- [ ] HTML **or** PDF export of notebook uploaded to repo
- [ ] Raw CSV files **NOT** committed *(use `.gitignore` to exclude the `data/` folder)*
- [ ] All code uses **relative paths** (`data/filename.csv`, not `/home/user/...`)
- [ ] Dashboard link is publicly accessible *(no login required — test in Incognito)*
- [ ] Presentation link is publicly accessible *(Anyone with link can view)*
- [ ] README updated with Executive Summary, all links, and technical notes
- [ ] Stories 1–4 all completed
- [ ] Candidate's Choice feature explained in README

---

## F. Repository Structure

```
veridi-logistics-audit/
│
├── veridi_logistics_audit.ipynb     ← Main analysis notebook
├── veridi_logistics_audit.pdf       ← pdf export (charts visible)
├── README.md                        ← This file
├── presentation/
│   └── veridi_delivery_audit.pptx  ← Slide deck
│
├── data/                            ←  NOT committed (in .gitignore)
│   ├── olist_orders_dataset.csv
│   ├── olist_order_reviews_dataset.csv
│   ├── olist_customers_dataset.csv
│   ├── olist_products_dataset.csv
│   ├── olist_order_items_dataset.csv
│   └── product_category_name_translation.csv
│
└── .gitignore
```

### `.gitignore` content :
```
data/
*.csv
__pycache__/
.ipynb_checkpoints/
```

---

## G. Dataset

**Source:** [Olist Brazilian E-Commerce Public Dataset — Kaggle](https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce)

The dataset contains 100,000 real anonymised orders from the Olist marketplace, covering October 2016 – September 2018 across all 27 Brazilian states. It is a relational database dump split across 9 CSV files connected by `order_id`, `customer_id`, `product_id`, and `seller_id` keys.




