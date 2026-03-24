FROM node:20-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY index.html vite.config.ts tsconfig.json postcss.config.cjs tailwind.config.cjs ./
COPY src/main.tsx src/index.css src/App.tsx ./src/
COPY src/api/index.ts ./src/api/index.ts
COPY src/store/editorStore.ts ./src/store/editorStore.ts
COPY src/components/ScenarioEditor.tsx ./src/components/ScenarioEditor.tsx
COPY src/components/EditorPage.tsx ./src/components/EditorPage.tsx
COPY src/components/ScenarioListPage.tsx ./src/components/ScenarioListPage.tsx
COPY src/components/FlowNodes.tsx ./src/components/FlowNodes.tsx
COPY src/components/NodePalette.tsx ./src/components/NodePalette.tsx
COPY src/components/NodePropertiesPanel.tsx ./src/components/NodePropertiesPanel.tsx
COPY src/components/SimulatorPanel.tsx ./src/components/SimulatorPanel.tsx
RUN npm run build:docker

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
