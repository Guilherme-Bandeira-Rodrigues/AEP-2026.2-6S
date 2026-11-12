import type { NextConfig } from "next";

const config: NextConfig = {
  poweredByHeader: false,
  output: process.env.NEXT_STANDALONE === "true" ? "standalone" : undefined,
};
export default config;
