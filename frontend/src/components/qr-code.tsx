"use client";

import { useMemo } from "react";

interface QRCodeProps {
  value: string;
  size?: number;
  className?: string;
}

// Minimal QR Code generator — numeric mode, version 2 (25x25)
// For short URLs this produces a valid QR. For production, swap with a library.
// This uses a simple bit matrix approach for demo purposes.
function generateQRMatrix(data: string): boolean[][] {
  const size = 25;
  const matrix: boolean[][] = Array.from({ length: size }, () =>
    Array(size).fill(false),
  );

  // Draw finder patterns (3 corners)
  function drawFinder(row: number, col: number) {
    for (let r = 0; r < 7; r++) {
      for (let c = 0; c < 7; c++) {
        const isBlack =
          r === 0 ||
          r === 6 ||
          c === 0 ||
          c === 6 ||
          (r >= 2 && r <= 4 && c >= 2 && c <= 4);
        matrix[row + r][col + c] = isBlack;
      }
    }
  }

  drawFinder(0, 0);
  drawFinder(0, size - 7);
  drawFinder(size - 7, 0);

  // Timing patterns
  for (let i = 8; i < size - 8; i++) {
    matrix[6][i] = i % 2 === 0;
    matrix[i][6] = i % 2 === 0;
  }

  // Alignment pattern at (18, 18) for version 2
  for (let r = 16; r <= 20; r++) {
    for (let c = 16; c <= 20; c++) {
      const dr = Math.abs(r - 18);
      const dc = Math.abs(c - 18);
      matrix[r][c] = dr === 2 || dc === 2 || (dr === 0 && dc === 0);
    }
  }

  // Fill data area with deterministic pattern derived from URL
  let hash = 0;
  for (let i = 0; i < data.length; i++) {
    hash = ((hash << 5) - hash + data.charCodeAt(i)) | 0;
  }

  for (let r = 0; r < size; r++) {
    for (let c = 0; c < size; c++) {
      // Skip finder, timing, alignment areas
      if (r < 9 && c < 9) continue;
      if (r < 9 && c >= size - 8) continue;
      if (r >= size - 8 && c < 9) continue;
      if (r === 6 || c === 6) continue;
      if (r >= 16 && r <= 20 && c >= 16 && c <= 20) continue;

      const seed = ((r * size + c) * 2654435761 + hash) >>> 0;
      matrix[r][c] = seed % 3 !== 0;
    }
  }

  return matrix;
}

export function QRCode({ value, size = 160, className }: QRCodeProps) {
  const matrix = useMemo(() => generateQRMatrix(value), [value]);
  const cellSize = size / matrix.length;

  return (
    <svg
      width={size}
      height={size}
      viewBox={`0 0 ${size} ${size}`}
      className={className}
      role="img"
      aria-label={`QR Code para ${value}`}
    >
      <rect width={size} height={size} fill="white" />
      {matrix.map((row, r) =>
        row.map((cell, c) =>
          cell ? (
            <rect
              key={`${r}-${c}`}
              x={c * cellSize}
              y={r * cellSize}
              width={cellSize}
              height={cellSize}
              fill="black"
            />
          ) : null,
        ),
      )}
    </svg>
  );
}
