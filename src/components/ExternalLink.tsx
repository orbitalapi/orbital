

interface ExternalLinkProps {
    href: string;
}

export default function ExternalLink(props: ExternalLinkProps) {
 
    return (
        <a target="_blank" href={props.href}>{props.href}</a>
    );
}