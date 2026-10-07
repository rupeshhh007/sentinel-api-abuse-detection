import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  workers: 1,
  timeout: 30000,
  use: {
    baseURL: "http://127.0.0.1:8080",
    viewport: { width: 1536, height: 1024 },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: {
        ...devices["Desktop Chrome"],
        viewport: { width: 1536, height: 1024 },
      },
    },
    { name: "installed-chrome", use: { channel: "chrome" } },
  ],
  webServer: {
    command:
      "java -jar ../target/springpractice-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo",
    url: "http://127.0.0.1:8080",
    reuseExistingServer: true,
    timeout: 60000,
  },
});
