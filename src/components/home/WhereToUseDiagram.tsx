import clsx from "clsx";


export default function WhereToUseDiagram({ className }) {

    return (
        <div className={clsx(className)}>
            <img src="/img/where-to-use.svg" className="mx-auto" alt="Diagram of where Orbital fits within a microservices stack" />
        </div>
    )
}