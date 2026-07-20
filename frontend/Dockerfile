# ============================================================================
# frontend/Dockerfile — TICKET-I126
# ============================================================================
# WHAT:    Multi-stage Docker build for the React UI.
# HOW:     Stage 1: node builds the SPA. Stage 2: nginx serves /dist.
# WHY:     Smaller final image (no Node in prod) + nginx handles SPA routing
#          + can reverse-proxy /api to the backend service.
# OBSERVE: `docker build .` succeeds; `docker run -p 8081:80 <image>` serves
#          the UI on localhost:8081.
# ============================================================================

# ---- Stage 1: build -------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

# Cache node_modules layer
COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Stage 2: nginx serve -------------------------------------------------
FROM nginx:1.27-alpine

# TODO(TICKET-I126 + TICKET-I128): copy a custom nginx.conf that:
#   - serves /dist
#   - fallbacks SPA routes to index.html (try_files $uri /index.html)
#   - reverse-proxies /api/* and /actuator/* to backend:8080
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
