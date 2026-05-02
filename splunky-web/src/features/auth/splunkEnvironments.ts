export type SplunkEnvironmentOption = {
  code: 'DEV' | 'PROD_ON_PREM' | 'PROD_AWS'
  label: string
  url: string
}

export const splunkEnvironmentOptions: SplunkEnvironmentOption[] = [
  {
    code: 'DEV',
    label: 'DEV',
    url: 'https://digital-splunk-search.hk.zzzz:8089',
  },
  {
    code: 'PROD_ON_PREM',
    label: 'Prod - On-prem',
    url: 'https://splunk-onprem.example.com:8089',
  },
  {
    code: 'PROD_AWS',
    label: 'Prod - AWS',
    url: 'https://splunk-aws.example.com:8089',
  },
]
