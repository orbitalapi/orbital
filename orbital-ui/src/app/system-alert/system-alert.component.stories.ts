import { moduleMetadata, Meta, StoryFn } from '@storybook/angular';
import { SystemAlertComponent, SystemAlert } from './system-alert.component';

export default {
  title: 'System Alert',
  component: SystemAlertComponent,
  decorators: [
    moduleMetadata({
      imports: [SystemAlertComponent], // Add necessary modules here
    }),
  ],
  argTypes: {
    alert: {
      control: 'object',
      description: 'The system alert to display.',
    },
  },
} as Meta;

const createAlert = (severity: SystemAlert['severity'], message: string, actionLabel: string | null = null, handler: (() => void) | null = null): SystemAlert => ({
  id: severity,
  message,
  actionLabel,
  handler,
  severity,
});

// Story for an Info Alert
export const InfoAlert: StoryFn = (args) => ({
  props: args,
  template: `<app-system-alert [alert]="alert"></app-system-alert>`,
});
InfoAlert.args = {
  alert: createAlert(
    'Info',
    'This is an informational alert.',
    'Learn more',
    () => alert('Info action clicked!')
  ),
};

// Story for a Warning Alert
export const WarningAlert: StoryFn = (args) => ({
  props: args,
  template: `<app-system-alert [alert]="alert"></app-system-alert>`,
});
WarningAlert.args = {
  alert: createAlert(
    'Warning',
    'This is a warning alert.',
    'Check settings',
    () => alert('Warning action clicked!')
  ),
};

// Story for an Error Alert
export const ErrorAlert: StoryFn = (args) => ({
  props: args,
  template: `<app-system-alert [alert]="alert"></app-system-alert>`,
});
ErrorAlert.args = {
  alert: createAlert(
    'Error',
    'This is an error alert.',
    'Fix issue',
    () => alert('Error action clicked!')
  ),
};

// Story for an Alert Without Action
export const NoActionAlert: StoryFn = (args) => ({
  props: args,
  template: `<app-system-alert [alert]="alert"></app-system-alert>`,
});
NoActionAlert.args = {
  alert: createAlert('Info', 'This alert has no action.', null, null),
};
