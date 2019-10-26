"use strict";
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
    return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (_) try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [op[0] & 2, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
var core = __importStar(require("@actions/core"));
var graphql_1 = require("@octokit/graphql");
var AddComment = /** @class */ (function () {
    function AddComment() {
    }
    AddComment.prototype.getOwnerAndRepo = function () {
        console.log("process.env.GITHUB_REPOSITORY " + process.env.GITHUB_REPOSITORY);
        if (process.env.GITHUB_REPOSITORY) {
            return process.env.GITHUB_REPOSITORY.split('/');
        }
        else {
            console.log("Error in getOwnerAndRepo");
            throw new Error('not able to obtain GITHUB_REPOSITORY from process.env');
        }
    };
    AddComment.prototype.addPullRequestCommentMutation = function () {
        return "mutation AddPullRequestComment($subjectId: ID!, $body: String!) {\n  addComment(input:{subjectId:$subjectId, body: $body}) {\n    commentEdge {\n        node {\n        createdAt\n        body\n      }\n    }\n    subject {\n      id\n    }\n  }\n}";
    };
    AddComment.prototype.getPullNumber = function () {
        console.log("process.env.GITHUB_REF " + process.env.GITHUB_REF);
        if (process.env.GITHUB_REF) {
            return parseInt(process.env.GITHUB_REF.split('/')[2]);
        }
        else {
            throw new Error('GITHUB_REF is missing in process.env');
        }
    };
    AddComment.prototype.findPullRequestQuery = function () {
        return "query FindPullRequestID ($owner: String!, $repo: String!, $pullNumber: Int!){\n  repository(owner:$owner, name:$repo) {\n    pullRequest(number:$pullNumber) {\n      id\n    }\n  }\n}";
    };
    AddComment.prototype.addCommentUsingSubjectId = function (pullRequestId, comment) {
        return __awaiter(this, void 0, void 0, function () {
            var data, token, graphQlResponse;
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        console.log("pullRequestId  ===>>>> " + pullRequestId);
                        data = JSON.parse(JSON.stringify(pullRequestId));
                        console.log("Parsed pull request id " + data);
                        token = core.getInput('repo-token');
                        console.log("repo-token " + token);
                        graphQlResponse = graphql_1.graphql(this.addPullRequestCommentMutation(), {
                            headers: {
                                authorization: "token " + token,
                            },
                            subjectId: data.repository.pullRequest.id,
                            body: comment,
                        });
                        console.log("Adding the comment ...");
                        return [4 /*yield*/, graphQlResponse];
                    case 1: return [2 /*return*/, _a.sent()];
                }
            });
        });
    };
    AddComment.prototype.getSubjectId = function (findPullRequestIdQuery, nameAndRepo) {
        return __awaiter(this, void 0, void 0, function () {
            var token, newVar;
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        console.log('Inside getSubjectId');
                        token = core.getInput('repo-token');
                        console.log("repo-token " + token);
                        return [4 /*yield*/, graphql_1.graphql(findPullRequestIdQuery, {
                                headers: {
                                    authorization: "token " + token,
                                },
                                owner: nameAndRepo[0],
                                repo: nameAndRepo[1],
                                pullNumber: this.getPullNumber(),
                            })];
                    case 1:
                        newVar = _a.sent();
                        console.log("Exiting getSubject Id");
                        return [2 /*return*/, newVar];
                }
            });
        });
    };
    AddComment.prototype.addComment = function (comment) {
        return __awaiter(this, void 0, void 0, function () {
            var nameAndRepo, name, repo, findPullRequestIdQuery, subjectId, e_1;
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        console.log('Inside addComment');
                        nameAndRepo = this.getOwnerAndRepo();
                        name = nameAndRepo[0], repo = nameAndRepo[1];
                        console.log("Name is " + name + "  and repo is " + repo);
                        findPullRequestIdQuery = this.findPullRequestQuery();
                        _a.label = 1;
                    case 1:
                        _a.trys.push([1, 4, , 5]);
                        return [4 /*yield*/, this.getSubjectId(findPullRequestIdQuery, nameAndRepo)];
                    case 2:
                        subjectId = _a.sent();
                        return [4 /*yield*/, this.addCommentUsingSubjectId(subjectId, comment)];
                    case 3: return [2 /*return*/, _a.sent()];
                    case 4:
                        e_1 = _a.sent();
                        console.error(e_1);
                        return [3 /*break*/, 5];
                    case 5: return [2 /*return*/];
                }
            });
        });
    };
    return AddComment;
}());
exports.default = AddComment;
