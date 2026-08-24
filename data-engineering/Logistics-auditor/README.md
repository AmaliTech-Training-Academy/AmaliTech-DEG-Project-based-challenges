### A. The Executive Summary

Veridi Logistics engaged this audit to determine whether a spike in negative reviews stems from delivery delays. We built a unified dataset by joining orders, reviews, customers, and products data with zero duplicate rows introduced during merging. The results confirm the CEO’s suspicion: on-time deliveries average a review score of 4.21 out of 5, while late deliveries drop to 2.94, and severely delayed (“Super Late”) orders fall to just 1.78 — a clear, direct link between delivery delay and customer dissatisfaction. Geographically, the problem is regional rather than nationwide: states like Alabama (23.9% late), Maranhão (19.9%), and Roraima (19.6%) show dramatically higher late-delivery rates than the national pattern, pointing to specific distribution or logistics gaps rather than a systemic failure. Product category analysis further reveals that furniture and mattress items (15.8% late) and female clothing (14.0% late) are disproportionately delayed, likely due to size/handling complexity. We recommend Veridi prioritize logistics interventions in the worst-performing states and review fulfillment processes for bulky/furniture categories first.


### B. Project Links

- **Link to Notebook:** https://colab.research.google.com/drive/17wdqMqVxUsOALa_hiHBEViWPHJfNrmP9#scrollTo=hTP59EAtRIQ1

### C. Technical Explanation

Data Cleaning:
Orders with status canceled or unavailable were excluded from delay analysis since they were never delivered and would distort the late/on-time comparison. Missing delivery dates were treated the same way — flagged as “Not Delivered” rather than dropped, to preserve visibility of the full order volume.

Story 1 (Joins):
Orders, customers, reviews, and order_items/products were merged on their respective keys (customer_id, order_id, product_id). Reviews were deduplicated on order_id before merging to prevent the classic 1-to-many join bug. Post-merge, merged_full.duplicated().sum() confirmed 0 duplicate rows.

Story 2 (Delay Calculator):
Days_Difference was calculated as estimated minus actual delivery date. Orders delivered on or before the estimate were classified “On Time”; up to 5 days late as “Late”; beyond that as “Super Late.”

Story 3 (Geographic): 
Grouped delivered orders by customer_state, calculated % late per state, sorted descending, visualized as a bar chart. Alabama, Maranhão, and Roraima stood out as clear outliers.

Story 4 (Sentiment): 
Grouped by delivery_status and averaged review_score. The nearly 2.5-point drop between On Time and Super Late orders is the strongest evidence in the whole analysis.

Story 5 (Translation):
Merged product_category_name_translation.csv on product_category_name to get English category names for the dashboard.

Story 6 (Candidate’s Choice): 
Added a top-10 late-delivery-rate-by-category breakdown, since Operations needs to know whether the delay problem is purely geographic or also tied to specific product handling — furniture and clothing categories being clustered at the top suggests packaging/handling process, not just shipping distance.

