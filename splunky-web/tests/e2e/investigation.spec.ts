import { expect, test } from '@playwright/test'

test('starts a mock investigation and renders the workspace', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /login/i })).toBeVisible()
  await page.getByLabel(/environment/i).click()
  await page.getByText('DEV', { exact: true }).click()
  await page.getByLabel(/username/i).fill('zhong.zc')
  await page.getByLabel(/password/i).fill('session-only')
  await page.getByRole('button', { name: /start session/i }).click()

  await page.getByRole('button', { name: /investigate/i }).click()

  await expect(page.getByText(/AI Summary/i)).toBeVisible()
  await expect(
    page.getByText('hub-payment-propose-api timeout', { exact: true }),
  ).toBeVisible()
  await expect(page.getByText(/Splunk logs sent to AI/i)).toBeVisible()
  await expect(page.getByText(/Executed SPL/i)).toBeVisible()
})
