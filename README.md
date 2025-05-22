# 🔗 Identity Reconciliation API - https://identity-reconciliation-20aq.onrender.com/identify

This is a backend task for Bitespeed's identity reconciliation challenge. It identifies and links customer contact records based on shared email or phone number across multiple requests.

---

## ⚙️ Tech Stack

- **Backend**: Java + Spring Boot
- **Database**: PostgreSQL (hosted on Render)
- **Build Tool**: Maven
- **Hosting**: Docker + Render

---

## 🚀 Hosted Endpoint

### POST `/identify`
**Base URL:**  
https://identity-reconciliation-20aq.onrender.com

---

## 📬 Request Format

Content-Type: `application/json`

```json
{
  "email": "doc@hillvalley.edu",
  "phoneNumber": "123456"
}
