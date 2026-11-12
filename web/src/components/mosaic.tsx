export function Mosaic({ compact = false }: { compact?: boolean }) {
  return (
    <div
      className={`mosaic ${compact ? "mosaic-compact" : ""}`}
      aria-hidden="true"
    >
      <svg viewBox="0 0 480 480" fill="none">
        <g className="mosaic-piece piece-one">
          <path fill="#E6BC38" d="M0 0h160v160H0z" />
          <path fill="#237E93" d="M160 0A160 160 0 0 0 0 160h160z" />
          <path fill="#FAFAF7" d="M160 80a80 80 0 0 0-80 80h80z" />
        </g>
        <g className="mosaic-piece piece-two">
          <path fill="#DE6928" d="M160 0h160v160H160z" />
          <path fill="#E6BC38" d="M160 0h160a80 80 0 0 1-160 0" />
          <path fill="#F0D4CA" d="M160 160h160a80 80 0 0 0-160 0" />
        </g>
        <g className="mosaic-piece piece-three">
          <path fill="#237E93" d="M320 0h160v160H320z" />
          <path fill="#32AEB1" d="M480 0a160 160 0 0 0-160 160h160z" />
          <path fill="#FAFAF7" d="M480 80a80 80 0 0 0-80 80h80z" />
        </g>
        <g className="mosaic-piece piece-four">
          <path fill="#F0D4CA" d="M0 160h160v160H0z" />
          <path fill="#32AEB1" d="M0 240a80 80 0 0 1 160 0v80H0z" />
          <path fill="#237E93" d="M0 320h160a80 80 0 0 0-160 0" />
        </g>
        <g className="mosaic-piece piece-five">
          <path fill="#FAFAF7" d="M160 160h160v160H160z" />
          <circle cx="240" cy="240" r="80" fill="#E6BC38" />
          <path fill="#DE6928" d="M240 160a80 80 0 0 1 0 160z" />
          <circle cx="240" cy="240" r="29" fill="#FAFAF7" />
        </g>
        <g className="mosaic-piece piece-six">
          <path fill="#32AEB1" d="M320 160h160v160H320z" />
          <path fill="#237E93" d="M320 160a160 160 0 0 1 160 160H320z" />
          <path fill="#F0D4CA" d="M320 240a80 80 0 0 1 80 80h-80z" />
        </g>
        <g className="mosaic-piece piece-seven">
          <path fill="#E6BC38" d="M0 320h160v160H0z" />
          <path fill="#FAFAF7" d="M0 320h160A160 160 0 0 1 0 480z" />
          <path fill="#B92E2F" d="M0 320h80a80 80 0 0 1-80 80z" />
        </g>
        <g className="mosaic-piece piece-eight">
          <path fill="#237E93" d="M160 320h160v160H160z" />
          <path fill="#32AEB1" d="M160 400a80 80 0 0 1 160 0v80H160z" />
          <path fill="#E6BC38" d="M160 480a80 80 0 0 1 160 0z" />
        </g>
        <g className="mosaic-piece piece-nine">
          <path fill="#F0D4CA" d="M320 320h160v160H320z" />
          <path fill="#B92E2F" d="M480 320v160H320a160 160 0 0 1 160-160" />
          <path fill="#DE6928" d="M480 400v80h-80a80 80 0 0 1 80-80" />
        </g>
      </svg>
    </div>
  );
}
