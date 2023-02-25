import {Callout} from "@/components/docs/Callout";
import Link from "next/link";


export default function UnderConstruction() {
  return (
    <Callout type='note' title={`We're working on it...`}>
      <p>We're currently rebuilding our docs, and it looks like you've hit an area that's still under construction.</p>
      <p>While we finish this page off, reach out to chat to us - we're happy to answer any questions you might have.</p>

      <p>Grab us on <Link href={'https://join.slack.com/t/orbitalapi/shared_invite/zt-697laanr-DHGXXak5slqsY9DqwrkzHg'}>Slack</Link> or <Link href={'https://github.com/orbitalapi/orbital/discussions'}>Github Discussions</Link></p>

    </Callout>
  )
}
