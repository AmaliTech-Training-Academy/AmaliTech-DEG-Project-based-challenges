export const INITIAL_FLOW = [
  {
    id: '1',
    type: 'start',
    text: 'Welcome to Support. What is your issue?',
    position: { x: 520, y: 50 },
    options: [
      { label: 'Internet is down', nextId: '2' },
      { label: 'Billing Question', nextId: '3' },
    ],
  },
  {
    id: '2',
    type: 'question',
    text: 'Have you tried restarting your router?',
    position: { x: 260, y: 270 },
    options: [
      { label: "Yes, didn't work", nextId: '4' },
      { label: 'No, let me try', nextId: '5' },
    ],
  },
  {
    id: '3',
    type: 'question',
    text: 'Is this for a Personal or Business account?',
    position: { x: 790, y: 270 },
    options: [
      { label: 'Personal', nextId: '6' },
      { label: 'Business', nextId: '6' },
    ],
  },
  {
    id: '4',
    type: 'end',
    text: 'Please call 555-0199 for a technician visit.',
    position: { x: 80, y: 520 },
    options: [],
  },
  {
    id: '5',
    type: 'end',
    text: 'Restarting usually fixes it! Come back if it fails.',
    position: { x: 420, y: 520 },
    options: [],
  },
  {
    id: '6',
    type: 'end',
    text: 'Connecting you to a Billing Agent...',
    position: { x: 790, y: 520 },
    options: [],
  },
]
