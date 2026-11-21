FROM node:22-alpine AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG VITE_ACADEMIC_ROLE_SELECTOR=false
ENV VITE_ACADEMIC_ROLE_SELECTOR=${VITE_ACADEMIC_ROLE_SELECTOR}
RUN npm run build

FROM nginx:stable-alpine
COPY --from=build /workspace/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
