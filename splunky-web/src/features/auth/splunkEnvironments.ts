export type SplunkEnvironmentOption = {
  code: 'DEV' | 'PROD_ON_PREM' | 'PROD_AWS'
  label: string
  description: string
  url: string
}

export const splunkEnvironmentOptions: SplunkEnvironmentOption[] = [
  {
    code: 'DEV',
    label: 'DEV',
    description: 'Primary testing search head for day-to-day investigation.',
    url: 'https://digital-splunk-search.hk.zzzz:8089',
  },
  {
    code: 'PROD_ON_PREM',
    label: 'Prod - On-prem',
    description: 'On-prem production mirror for controlled troubleshooting.',
    url: 'https://splunk-onprem.example.com:8089',
  },
  {
    code: 'PROD_AWS',
    label: 'Prod - AWS',
    description: 'AWS production mirror for cloud-hosted service traces.',
    url: 'https://splunk-aws.example.com:8089',
  },
]
