import { createDemoServices } from "./demo";
import { createHttpServices } from "./http";

export const services =
  process.env.NEXT_PUBLIC_DATA_MODE === "demo"
    ? createDemoServices()
    : createHttpServices(
        process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api",
      );
