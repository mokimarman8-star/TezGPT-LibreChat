# Provider Findings for TezGPT Deployment

## Render API and hosting

Render API requests use an API key in the `Authorization: Bearer` header. The official API documentation shows `GET https://api.render.com/v1/services?limit=20` for listing services. Render web services can deploy from GitHub repositories and Dockerfiles, bind to `0.0.0.0`, and expose an `onrender.com` URL.

Render Free web services are intended for testing and hobby projects. They sleep after 15 minutes without inbound traffic, have an ephemeral filesystem, cannot scale beyond one instance, and can be suspended for unusually high outbound traffic. Free services are therefore unsuitable for millions of users.

Sources:

- https://render.com/docs/api
- https://api-docs.render.com/reference/authentication
- https://render.com/docs/web-services
- https://render.com/docs/free
- https://api-docs.render.com/reference/create-service
- https://api-docs.render.com/reference/create-deploy

## MongoDB Atlas

MongoDB Atlas Free clusters are intended for small-scale development. The official documentation states that a Free cluster uses 0.5 GB of storage, supports up to 500 connections and 100 operations per second, and has rolling seven-day transfer limits of 10 GB in and 10 GB out. Free clusters do not provide backups, sharding, private endpoints, or automatic storage scaling.

Sources:

- https://www.mongodb.com/docs/atlas/tutorial/deploy-free-tier-cluster/
- https://www.mongodb.com/docs/atlas/reference/free-shared-limitations/

## Cloud Run scale-up option

Google Cloud Run automatically scales container instances and applies monthly free allowances. The official pricing page lists a request-based allowance of 2 million requests per month plus CPU and memory allowances in the default region. Usage beyond the free allowance is billed, and database/storage services are separate.

Source:

- https://cloud.google.com/run/pricing

## Current deployment state

A Render Free web service named `tezgpt-librechat` was created from the public repository `https://github.com/mokimarman8-star/TezGPT-LibreChat` with service ID `srv-da3c9dbtqb8s73cj21sg` and public URL `https://tezgpt-librechat.onrender.com`. The initial deploy ID is `dep-da3c9djtqb8s73cj22jg`. The deployment reached `update_in_progress` during monitoring; the public URL did not respond before the health probe timeout. No credentials are stored in this document or repository.
