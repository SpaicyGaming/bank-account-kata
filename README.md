# Bank Account Kata

This is a TDD implementation written in Kotlin of the Bank Account Kata.
It offers a set of Web API endpoints for performing various basic bank account managing operations, including:

- Depositing funds into an account
- Withdrawing funds from an account
- Checking the account balance
- Printing the account transaction history

Main libraries used:

- Vert.X
- Jackson
- H2 Database Engine
- JUnit 5
- MockK
- AssertJ
- REST Assured
- Testcontainers (PostgreSQL)

### API Endpoints

#### Deposit funds into an account

```
POST /api/deposit

body:
{
  "uuid": "00000000-0000-0000-0000-000000000000",
  "amount": 100.00
}
```

#### Withdraw funds from an account

For this operation to succeed, the account must have sufficient funds.

```
POST /api/withdraw

body:
{
  "uuid": "00000000-0000-0000-0000-000000000000",
  "amount": 100.00K
}
```

#### Transaction history of an account

Gets the transaction history of an account, sorted by the time of the transaction in descending order.

```
GET /api/history/:uuid

response:
[ {
  "id" : "ab1a289b-aa03-4c6f-b6f7-5dc426df76c1",
  "operation" : "DEPOSIT",
  "amount" : 100,
  "time" : [ 2023, 7, 1, 15, 36, 59, 881840600 ]
}, {
  "id" : "ab1a289b-aa03-4c6f-b6f7-5dc426df76c1",
  "operation" : "WITHDRAWAL",
  "amount" : 50,
  "time" : [ 2023, 7, 2, 15, 36, 59, 881882187 ]
}, {
  "id" : "ab1a289b-aa03-4c6f-b6f7-5dc426df76c1",
  "operation" : "DEPOSIT",
  "amount" : 100,
  "time" : [ 2023, 7, 3, 15, 36, 59, 881894170 ]
} ]
```

#### Account balance

Gets the current balance of an account.

```
GET /api/balance/:uuid

response:
{
  "balance": 150.00
}
```