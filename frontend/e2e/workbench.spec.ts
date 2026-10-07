import { test, expect } from "@playwright/test";
const headers = { "X-Sentinel-Console": "sentinel" };
test("replay evidence, observe mode, and live provenance survive a late replay response", async ({
  page,
  request,
}) => {
  const consoleErrors: string[] = [];
  page.on("pageerror", (e) => consoleErrors.push(e.message));
  const current = await (await request.get("/api/console/policy")).json();
  await request.put("/api/console/policy", {
    headers,
    data: {
      ...current,
      mode: "ENFORCE",
      maxRequests: 30,
      sourceMaxRequests: 120,
      windowSeconds: 60,
      threshold: 60,
      failureMode: "OPEN",
    },
  });
  await request.delete("/api/console/replay", { headers });
  await page.goto("/");
  await expect(
    page.getByRole("heading", { name: "Traffic overview" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Run scenario", exact: true }).click();
  await page.getByRole("button", { name: "Run scenario", exact: true }).click();
  await expect(
    page.getByText("suspicious decisions / 80 requests"),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Run evaluation", exact: true })
    .click();
  await expect(
    page.getByText("False positives", { exact: true }),
  ).toBeVisible();
  await expect(
    page.locator(".evaluation-stats>div").nth(2).locator("b"),
  ).toHaveText("0");
  await page
    .getByRole("button", { name: "Investigations", exact: true })
    .click();
  await page
    .getByRole("combobox", { name: "Filter action" })
    .selectOption("BLOCK");
  await page
    .getByRole("button", { name: /Inspect decision/ })
    .first()
    .click();
  await expect(
    page.getByRole("heading", { name: "Source timeline" }),
  ).toBeVisible();
  const exported = page.waitForEvent("download");
  await page.getByRole("button", { name: "Export this decision" }).click();
  expect((await exported).suggestedFilename()).toMatch(/^decision-.*\.json$/);
  await page.keyboard.press("Escape");
  await expect(page.getByRole("dialog")).toHaveCount(0);
  await page.getByRole("button", { name: "Policies", exact: true }).click();
  await page.getByLabel("Enforcement mode").selectOption("OBSERVE");
  await page.getByRole("button", { name: "Apply policy" }).click();
  await expect(page.getByRole("status")).toContainText("Policy applied");
  await page.getByRole("button", { name: "Attack lab", exact: true }).click();
  await page.getByRole("button", { name: "Run scenario", exact: true }).click();
  await expect(
    page.locator(".result-stats>div").nth(1).locator("b"),
  ).toHaveText("0");
  await expect(
    page.locator(".result-stats>div").nth(2).locator("b"),
  ).toHaveText("50");
  await page.getByRole("button", { name: "Overview", exact: true }).click();
  await expect(page.locator(".metric").first().locator("strong")).toHaveText(
    "160",
  );
  const before = await (
    await request.get("/api/console/overview?origin=LIVE")
  ).json();
  let release: () => void = () => {};
  let arrived: () => void = () => {};
  let finished: () => void = () => {};
  const gate = new Promise<void>((resolve) => {
    release = resolve;
  });
  const pending = new Promise<void>((resolve) => {
    arrived = resolve;
  });
  const done = new Promise<void>((resolve) => {
    finished = resolve;
  });
  await page.route("**/api/console/overview?origin=REPLAY", async (route) => {
    const response = await route.fetch();
    arrived();
    await gate;
    await route.fulfill({ response });
    finished();
  });
  await page
    .getByRole("button", { name: "Refresh traffic", exact: true })
    .click();
  await pending;
  await page.getByRole("button", { name: "Send live probe" }).click();
  const expected = String(before.telemetry.total + 1);
  await expect(page.locator(".metric").first().locator("strong")).toHaveText(
    expected,
  );
  release();
  await done;
  await expect(page.locator(".metric").first().locator("strong")).toHaveText(
    expected,
  );
  await expect(page.getByText(/Protected endpoint traffic/)).toBeVisible();
  await page.unrouteAll({ behavior: "wait" });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await expect(
    page.getByRole("heading", { name: "Traffic activity" }),
  ).toBeVisible();
  expect(consoleErrors).toEqual([]);
});
