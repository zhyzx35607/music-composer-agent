import { Mic } from 'lucide-react'

export default function CaptionDisplay({ caption }: { caption: string }) {
  return (
    <div className="relative overflow-hidden rounded-2xl p-6 border border-border shadow-lg bg-gradient-to-br from-primary/10 to-secondary/10">
      {/* Decorative background elements */}
      <div className="absolute top-0 right-0 -mt-4 -mr-4 h-32 w-32 rounded-full bg-accent/20 blur-2xl" />
      <div className="absolute bottom-0 left-0 -mb-4 -ml-4 h-24 w-24 rounded-full bg-primary/20 blur-2xl" />

      <div className="relative z-10 flex items-center gap-3 mb-4">
        <div className="h-10 w-10 rounded-xl bg-accent flex items-center justify-center shadow-lg shadow-accent/25">
          <Mic className="h-6 w-6 text-on-primary" />
        </div>
        <div>
          <h3 className="text-lg font-semibold text-foreground">英文描述 (Caption)</h3>
          <p className="text-xs text-muted-foreground">Text2MIDI Input</p>
        </div>
      </div>

      <div className="bg-background/50 rounded-xl p-4 border border-border/50">
        <p className="text-foreground italic leading-relaxed text-lg">"{caption}"</p>
      </div>
    </div>
  )
}
