# TezGPT Free Staging and Scale Plan

## Decision

The zero-cost starting point for the complete LibreChat stack is a **Render Free web service** connected to a **MongoDB Atlas Free cluster**. This is suitable for development, demonstrations, and early testing only. It is not a production architecture for millions of users.

Render documents that Free web services can host Node applications, but they spin down after 15 minutes without inbound traffic, use an ephemeral filesystem, cannot scale beyond one instance, and may be suspended for unusually high outbound traffic. Render also states that Free instances are not intended for production use.[1]

MongoDB Atlas Free clusters are intended for small-scale development. They never expire by default, but the free tier is limited to 0.5 GB of storage, 100 operations per second, 500 connections, and 10 GB of inbound and 10 GB of outbound transfer in a rolling seven-day period. Free clusters do not provide backups, sharding, private endpoints, or automatic storage scaling.[2]

## Required staging configuration

| Component | Free staging choice | Required secret or setting |
|---|---|---|
| Web/API | Render Free Web Service using the Dockerfile in this repository | Render service linked to `mokimarman8-star/TezGPT-LibreChat`, branch `main`. |
| Database | MongoDB Atlas Free cluster | `MONGO_URI` supplied as a Render secret environment variable. |
| Authentication | LibreChat server JWT/session secrets | Render-generated `JWT_SECRET`, `JWT_REFRESH_SECRET`, `CREDS_KEY`, and `CREDS_IV`. |
| Files | External object storage or temporary testing only | Do not rely on the Render filesystem for persistent user files. |
| Search | Optional during staging | Meilisearch or the server’s configured search provider can be added later. |

No provider key, MongoDB password, JWT secret, or Render API key belongs in this repository. Secrets must be entered in the hosting dashboard or secret manager.

## Why this cannot support millions for free

A million-user product needs more than a larger web process. It needs multiple stateless API instances, a highly available and horizontally scalable database, a shared cache and queue, durable object storage, CDN delivery, observability, abuse controls, provider rate-limit management, and a tested disaster-recovery plan. Free Render and Atlas tiers intentionally lack those guarantees and limits. The application can be designed so the web tier remains stateless and can later move to Cloud Run, Kubernetes, or a paid Render plan, but the database and storage tiers must be upgraded before serious traffic.

Google Cloud Run is a credible later web-tier option because it automatically scales container instances and applies a monthly free allowance, but usage beyond the free allowance is billed and the database is separate. The official pricing page lists a request-based free allowance of 2 million requests per month plus CPU and memory allowances in the default region.[3] This is a staging or low-traffic allowance, not free capacity for millions of users.

## Next deployment requirement

The Render web service cannot become a working LibreChat instance until a MongoDB connection string is available. The user may either provide a MongoDB Atlas connection string or create an Atlas Free cluster and add its connection string to Render as `MONGO_URI`. After that value is available, the web service can be deployed without putting the credential in GitHub.

## References

[1]: https://render.com/docs/free "Render Free services and limitations"
[2]: https://www.mongodb.com/docs/atlas/reference/free-shared-limitations/ "MongoDB Atlas Free cluster limitations"
[3]: https://cloud.google.com/run/pricing "Google Cloud Run pricing and free tier"
