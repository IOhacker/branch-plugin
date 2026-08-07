#   Loan Repayment Plugin for Apache Fineract / Mifos

Plugin that implements the **  branch connector** for loan repayments using refund references.

Aligned with:

- OpenAPI contract ` _Mifos_Contrato_API.yaml`
- Technical document *Integración de Sucursales   con Mifos*
- Test matrix (56 cases: consultation, payment, reference, reconciliation)
- Postman collection provided by  

## What it does

| Capability | Endpoint(s) |
|---|---|
| Client file | `GET /v1.0/clientes/expediente/{identificador}` |
| Client loans | `GET /v1.0/clientes/cuentas/{identificador}/accounts` |
| Loan balance | `GET /v1.0/creditos/balance/{identificador}` |
| Repayment schedule | `GET /v1.0/creditos/calendario/{identificador}` |
| Repayment by refund reference (current) | `POST /v1.0/creditos/pagos/{referencia}/transactions?command=repayment` |
| Resolve refund reference **[proposed]** | `GET /v1.0/referencias/reembolso/{referencia}` |
| Validate payment **[proposed]** | `POST /v1.0/referencias/reembolso/{referencia}/validate` |
| Atomic branch payment **[proposed]** | `POST /v1.0/sucursales/pagos` |
| Query by externalId **[proposed]** | `GET /v1.0/sucursales/pagos/{externalId}` |
| Reverse payment **[proposed]** | `POST /v1.0/sucursales/pagos/{externalId}/reversal` |
| Reconciliation report **[proposed]** | `GET /v1.0/sucursales/{branchId}/pagos?fromDate=&toDate=` |

## Critical design rules (from   specs)

1. **Refund references, account numbers and externalIds are always treated as text** — never as numbers (avoids scientific notation / leading-zero loss).
2. **Idempotency**: `externalId` / `Idempotency-Key` identifies a logical payment. Retries with the same key return the original result and never create a second transaction.
3. **Timeouts** leave the local record in status `UNKNOWN`. The branch system must query by `externalId` before any retry.
4. **Multiple loans** on the same client require explicit selection; payment is always applied to the loan resolved from the refund reference.
5. **Overpayment** is rejected by default (`OVERPAYMENT_NOT_ALLOWED`) until product policy is confirmed.

## Architecture

```
Branch system  ──HTTPS──►    Plugin (this JAR)
                              │
                              ├─ ClientLoanQueryService   (read m_client / m_loan / schedule)
                              ├─ RefundReferenceService   (resolve reference → loan)
                              ├─ BranchPaymentService     (orchestration + audit)
                              ├─ FineractRepaymentGateway (command=repayment into core)
                              └─ m_branch_branch_payment (local audit / recon)
                                       │
                                       ▼
                              Apache Fineract core (loan accounting & schedule)
```

## Build

```bash
./mvnw clean package -Dmaven.test.skip=true
```

Produces: `target/branch-loan-repayment-plugin-1.0.0-SNAPSHOT.jar`

## Deploy

```bash
# Docker / Spring Boot loader
mkdir -p /opt/fineract/plugins
cp target/branch-loan-repayment-plugin-*.jar /opt/fineract/plugins/
java -Dloader.path=/opt/fineract/plugins/ -jar fineract-provider.jar

# or Tomcat
cp target/branch-loan-repayment-plugin-*.jar \
   $TOMCAT_HOME/webapps/fineract-provider/WEB-INF/lib/
```

Liquibase will create `m_branch_branch_payment` on each tenant at startup.

## Pending confirmations (see document §15)

| Item | Action required |
|---|---|
| Storage of refund reference | Confirm table/column or Data Table in Mifos (`RefundReferenceService` currently probes `m_loan.external_id`) |
| `paymentTypeId` catalogue | Confirm IDs for cash / transfer used by branches |
| Overpayment policy | Keep reject, allow credit-balance, or product-specific |
| Reversal endpoint | Wire `FineractRepaymentGateway.reverseRepayment` to the correct Fineract service |
| AuthN | OAuth2 client-credentials / mTLS between branch system and connector |

## Scaffold vs production

`FineractRepaymentGateway` currently contains a **scaffold** implementation so the plugin compiles and the orchestration/idempotency layer can be unit-tested. Before production:

1. Inject `LoanWritePlatformService` (or the portfolio command source).
2. Call `makeLoanRepayment(loanId, jsonCommand, isRecoveryRepayment)`.
3. Return the real `resourceId` from `CommandProcessingResult`.

## License

Mozilla Public License 2.0 (same family as the Mifos savings-plugin reference).
