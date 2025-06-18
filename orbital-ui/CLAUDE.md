# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

This is an Angular 17 application that contains three distinct applications:
- **Main UI** (`src/app/`): The primary UI for Orbital platform 
- **Taxi Playground/Voyager** (`src/taxi-playground-app/`): Interactive query playground deployed at voyager.orbitalhq.com

## Build Commands

### Development
- `npm run start` - Start main app with proxy config
- `npm run start-dev` - Start with dev configuration and increased memory
- `npm run build` - Build main app with increased memory allocation
- `npm run build-prod` - Production build for vyne-app
- `npm run build-prod-playground` - Production build for taxi-playground

### Testing & Quality
- `npm test` - Run unit tests
- `npm run lint` - Run ESLint
- `npm run e2e` - Run end-to-end tests
- `npm run automation` - Run Cypress UI tests

### Development Tools
- `npm run storybook` - Start Storybook component library
- `npm run build-storybook` - Build Storybook for deployment

## Architecture

### Multi-Application Structure
The codebase uses Angular's multi-project configuration with shared components and services:

- **Shared Components**: Most UI components in `src/app/` are reusable across applications
- **Application-Specific**: Each app has its own entry point, routing, and specific features
- **Common Services**: Core services like `TypesService`, `QueryService` in `src/app/services/`

### Key Architectural Patterns

**Module-Based Organization**: Each major feature is organized as an Angular module with components, services, and routing

**Service-Oriented**: Core business logic is centralized in services:
- `TypesService` - Schema and type management
- `QueryService` - Query execution and management  
- `WebsocketService` - Real-time communication
- `AuthService` - Authentication handling

**Component Hierarchy**: Complex features use container/presenter pattern with dedicated modules

### UI Framework Stack
- **Angular 17** with standalone components
- **Taiga UI** as primary component library
- **Angular Material** for specific components, but is considered deprecated and being phased out.
- **Monaco Editor** for code editing with language server integration
- **Custom SCSS** for component-specific styles

### Angular code style
- Use standalone components for any new work
- Favour inline templates (rather than a separate.html file) for simple components
- Don't write tests
- Favour signals over @Input()
- scss should almost always add `@import "src/common";` at the top, plus other imports as required.
- Tailwind is not (currently) available, but all tailwind colors are included as scss variables via imports, (eg: `$slate-500`).
  - Use tailwind colors throughout. Use slate-xx as the palette for greys
  - Favour sizing in rems over pixels. General spacing rhythm (for margin, padding and flex gaps) is spacing in units of 0.5rem

### Build Configuration
Uses custom webpack config for:
- Monaco Editor integration with proper asset handling
- Memory optimization for large builds
- Custom CSS/asset processing for Monaco and VSCode components

### Proxy Configuration
Development uses proxy config (`proxy.conf.json`) to route API calls to backend services

## Key Development Patterns

### Component Structure
- Components follow Angular style guide naming conventions
- SCSS files co-located with components
- Story files (.stories.ts) for Storybook documentation
- Module files organize related components and dependencies

### State Management
- Services use RxJS subjects/observables for state
- Local component state for UI-specific concerns
- WebSocket integration for real-time updates

### Type Safety
- Strong TypeScript usage throughout
- Custom interfaces in `src/app/services/models.ts` and component-specific files
- Schema definitions in `src/app/services/schema.ts`

### Testing Strategy  
- Karma/Jasmine for unit tests
- Cypress for E2E testing with page object pattern in `cypress/page-objects/`
- Storybook for component documentation and testing

## Application-Specific Notes

### Environment Configuration
- `src/environments/` contains environment-specific configs
- UI customizations in `src/environments/ui-customisations.ts`
- App type configuration supports 'vyne' | 'orbital' modes

### Memory Requirements
Build processes require increased Node.js memory allocation (configured in package.json scripts)
