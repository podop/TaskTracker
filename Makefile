SHELL := /usr/bin/env bash
.DEFAULT_GOAL := help

ROOT := $(shell pwd)
BACKEND := $(ROOT)/backend
FRONTEND := $(ROOT)/frontend

.PHONY: help up down logs backend frontend build test backend-test frontend-test e2e clean

help:
	@echo "Available targets:"
	@echo "  up         docker compose up --build -d"
	@echo "  down       docker compose down"
	@echo "  logs       docker compose logs -f"
	@echo "  backend    Run the backend locally with Maven"
	@echo "  frontend   Run the frontend dev server with Vite"
	@echo "  build      Build both backend (mvn package) and frontend (vite build)"
	@echo "  test       Run the full test cycle (scripts/run-all-tests.sh)"
	@echo "  backend-test  Run backend unit + integration tests"
	@echo "  frontend-test Run frontend Vitest + Playwright"
	@echo "  e2e        Run Playwright end-to-end tests only"
	@echo "  clean      Remove build artifacts"

up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f

backend:
	cd $(BACKEND) && mvn spring-boot:run

frontend:
	cd $(FRONTEND) && npm install --no-audit --no-fund && npm run dev

build:
	cd $(BACKEND) && mvn -B -ntp -DskipTests package
	cd $(FRONTEND) && npm install --no-audit --no-fund && npm run build

test:
	./scripts/run-all-tests.sh

backend-test:
	cd $(BACKEND) && mvn -B -ntp verify

frontend-test:
	cd $(FRONTEND) && npm run test && npm run test:e2e

e2e:
	cd $(FRONTEND) && npm run test:e2e

clean:
	cd $(BACKEND) && mvn -B -ntp clean
	rm -rf $(FRONTEND)/dist $(FRONTEND)/playwright-report $(FRONTEND)/test-results
