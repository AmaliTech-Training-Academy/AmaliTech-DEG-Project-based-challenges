FROM node:20-alpine

# Set working directory
WORKDIR /usr/src/app

# Copy package files
COPY app/package.json ./

# Install dependencies
RUN npm install

# Copy application source code
COPY app/ ./

# Create a non-root user and change ownership
# The node image already has a 'node' user built-in
RUN chown -R node:node /usr/src/app

# Switch to the non-root user
USER node

# Start the application
CMD ["npm", "start"]
