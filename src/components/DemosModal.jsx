import { Dialog, Transition } from '@headlessui/react';
import { Fragment, useState } from 'react';
import clsx from 'clsx';

export default function WatchADemoButton(props) {
    const [playerVisible, setPlayerVisible] = useState(false);

    return (
        <>
            <Transition.Root show={playerVisible} as={Fragment}>
                <Dialog as='div' className='relative z-10' onClose={setPlayerVisible}>
                    <Transition.Child
                        as={Fragment}
                        enter='ease-out duration-300'
                        enterFrom='opacity-0'
                        enterTo='opacity-100'
                        leave='ease-in duration-200'
                        leaveFrom='opacity-100'
                        leaveTo='opacity-0'
                    >
                        <div className='fixed inset-0 bg-slate-800 bg-opacity-75 backdrop-blur-sm  transition-opacity' />
                    </Transition.Child>
                    <DemosModel defaultVideo={props.defaultVideo}></DemosModel>

                </Dialog>
            </Transition.Root>

            <button
                className='bg-midnight-blue hover:bg-slate-300/20 color-white text-white font-bold h-12 px-6 rounded-full border-2 border-white sm:w-full w-60 flex items-center justify-center uppercase tracking-wider'
                onClick={() => setPlayerVisible(true)}
            >
                Watch a demo {'>'}
            </button>
        </>
    )
}

export function DemosModel(props) {
    const videos = [
        {
            key: 'intro',
            title: 'Introduction',
            duration: '2 mins',
            link: 'https://www.loom.com/embed/611473f78fa54235bc70e5d0dea13534'
        },
        {
            key: 'breakingChanges',
            title: 'Handling breaking changes with Orbital',
            duration: '3 mins',
            link: 'https://www.loom.com/embed/ff01b2a3655047c58f60240aaae54445'
        }
    ];

    let defaultVideo = (props.defaultVideo && videos.find(video => video.key === props.defaultVideo)) || videos[0]

    const [currentVideo, setCurrentVideo] = useState(defaultVideo);

    function onVideoClicked(event, video) {
        event.stopPropagation();
        event.nativeEvent.stopImmediatePropagation();
        event.preventDefault();
        setCurrentVideo(video);
    }


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
                        <iframe className='w-[80%]' src={currentVideo.link}
                            frameBorder='0' webkitallowfullscreen mozallowfullscreen allowFullScreen></iframe>
                        <div className='flex-grow px-8 py-12'>
                            <h2 className='text-3xl text-slate-50 font-extrabold py-8'>Demos</h2>
                            {videos.map(video => {
                                return (<div key={video.link} className='mb-8'>
                                    <a onClick={(e) => onVideoClicked(e, video)} href=''>
                                        <h3 className={clsx(
                                            'text-xl text-slate-50 mb-1',
                                            video.link === currentVideo.link ? 'text-citrus font-bold' : 'text-slate-50 hover:text-sky-300'
                                        )}>{video.title}</h3>
                                    </a>
                                    {
                                        (video.link === currentVideo.link) && (
                                            <div className={'flex items-center text-citrus mb-2 text-sm font-bold'}>
                                                <svg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' strokeWidth={1.5}
                                                    stroke='currentColor' className='w-4 h-4 mr-4'>
                                                    <path strokeLinecap='round' strokeLinejoin='round'
                                                        d='M5.25 5.653c0-.856.917-1.398 1.667-.986l11.54 6.348a1.125 1.125 0 010 1.971l-11.54 6.347a1.125 1.125 0 01-1.667-.985V5.653z' />
                                                </svg>
                                                <span>Now playing</span>

                                            </div>
                                        )}

                                    <div className='flex items-center'>
                                        <svg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' strokeWidth='1.5'
                                            stroke='currentColor' className='w-4 h-4 mr-4'>
                                            <path strokeLinecap='round' strokeLinejoin='round'
                                                d='M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z' />
                                        </svg>
                                        <span>{video.duration}</span>
                                    </div>


                                </div>);
                            })}

                        </div>
                    </div>

                </Dialog.Panel>
            </Transition.Child>
        </div>
    </div>);
}