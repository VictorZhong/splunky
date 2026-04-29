import { expect, test } from '@playwright/test'

test('starts a mock investigation and renders the workspace', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: /investigate/i }).click()

  await expect(page.getByText(/Diagnosis Summary/i)).toBeVisible()
  await expect(
    page.getByText('hub-payment-propose-api timeout', { exact: true }),
  ).toBeVisible()
  await expect(page.getByText(/AI Assistant/i)).toBeVisible()
})
