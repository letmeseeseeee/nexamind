FROM node:20-alpine AS builder

WORKDIR /app

COPY nexamind-frontend/package.json nexamind-frontend/package-lock.json ./
RUN npm ci --legacy-peer-deps

COPY nexamind-frontend/ ./

ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV NEXT_TELEMETRY_DISABLED=1

RUN npm run build

FROM node:20-alpine AS runner

RUN addgroup -S nodejs \
    && adduser -S -G nodejs nextjs

WORKDIR /app

COPY --from=builder --chown=nextjs:nodejs /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV HOSTNAME=0.0.0.0
ENV PORT=3000

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=5 \
    CMD wget -qO- "http://localhost:${PORT}/login" >/dev/null || exit 1

CMD ["node", "server.js"]
