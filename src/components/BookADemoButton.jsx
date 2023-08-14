import {Dialog, Transition} from '@headlessui/react';
import {Fragment, useState} from 'react';
import clsx from 'clsx';

export default function BookADemoButton(props) {
  const [calendarVisible, setCalendarVisible] = useState(false);

  return (
    <>
      <Transition.Root show={calendarVisible} as={Fragment}>
        <Dialog as='div' className='relative z-10' onClose={setCalendarVisible}>
          <Transition.Child
            as={Fragment}
            enter='ease-out duration-300'
            enterFrom='opacity-0'
            enterTo='opacity-100'
            leave='ease-in duration-200'
            leaveFrom='opacity-100'
            leaveTo='opacity-0'
          >
            <div className='fixed inset-0 bg-slate-800 bg-opacity-75 backdrop-blur-sm  transition-opacity'/>
          </Transition.Child>
          <CalendarModal></CalendarModal>
        </Dialog>
      </Transition.Root>

      <button
        className='bg-midnight-blue hover:bg-slate-300/20 color-white text-white font-bold h-12 px-6
                rounded-lg border-2 border-white  flex sm:flex-1 items-center justify-center'
        onClick={() => setCalendarVisible(true)}
      >
        Book a demo
      </button>
    </>
  )
}


export function CalendarModal(props) {


  return (<div className='fixed inset-0 z-10 overflow-y-auto'>
    <div className='flex min-h-full items-center justify-center p-4 text-center sm:items-center sm:p-0'>
      <Transition.Child
        as={Fragment}
        enter='ease-out duration-300'
        enterFrom='opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95'
        enterTo='opacity-100 translate-y-0 sm:scale-100'
        leave='ease-in duration-200'
        leaveFrom='opacity-100 translate-y-0 sm:scale-100'
        leaveTo='opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95'
      >
        <Dialog.Panel
          className='relative transform overflow-hidden rounded-lg bg-slate-800 px-4 pt-5 pb-4 text-left shadow-2xl transition-all sm:my-8 sm:w-full sm:max-w-sm sm:p-6 w-[75vw] h-[75vh]'>
          <div className='flex w-full h-full'>
            <iframe className='w-full'
                    src='https://calendar.google.com/calendar/appointments/schedules/AcZssZ0ihMtHrlqo-9Zu2041JizUvJv-rk8m2l88UtiTI14c-dtv8ZVrnd_p1dLnmMyFFKc1tAF2ig41?gv=true'
                    frameBorder='0' webkitallowfullscreen mozallowfullscreen allowFullScreen></iframe>
          </div>

        </Dialog.Panel>
      </Transition.Child>
    </div>
  </div>);
}
