import {getWrapper} from "../utils/client";

export type FrontendConfigCommand = {
    id: string,
    name: string,
    blocksWsadmin: boolean
}

export type FrontendConfigConfig = {
    id: string
}

export type FrontendConfigLog = {
    id: string
}

export type GetFrontendConfigResponse = {
    version: string,
    commands: FrontendConfigCommand[],
    servers: number[],
    configs: FrontendConfigConfig[],
    logs: FrontendConfigLog[]
}

export const getFrontendConfig = (
): Promise<GetFrontendConfigResponse> =>
    getWrapper(`api/front`)